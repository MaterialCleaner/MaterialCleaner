package me.gm.cleaner.server;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import api.SystemService;
import me.gm.cleaner.client.CleanerHooksClient;
import me.gm.cleaner.server.observer.BaseIntentObserver;
import me.gm.cleaner.server.observer.ObserverManager;

public class PackageReceiver {
    private final Set<String> mInstalledPackages = new HashSet<>();
    private final CleanerServer mServer;

    public PackageReceiver(final CleanerServer server) {
        mServer = server;
    }

    public void registerPackageReceiver() {
        final var observer = ObserverManager.INSTANCE.getObserver(BaseIntentObserver.class);
        if (observer == null) {
            return;
        }
        SystemService.getInstalledPackagesFromAllUsersNoThrow(0)
                .stream()
                .map(pi -> pi.packageName)
                .collect(Collectors.toCollection(() -> mInstalledPackages));
        final var intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addDataScheme("package");
        observer.registerReceiver(
                new IIntentReceiver.Stub() {
                    @Override
                    public void performReceive(final Intent intent, final int resultCode,
                                               final String data, final Bundle extras,
                                               final boolean ordered, final boolean sticky,
                                               final int sendingUser) {
                        final var packageName = intent.getData().getSchemeSpecificPart();
                        switch (intent.getAction()) {
                            case Intent.ACTION_PACKAGE_ADDED:
                                if (mInstalledPackages.add(packageName)) {
                                    mServer.broadcastIntentDelayed(broadcastIntent -> broadcastIntent
                                            .setAction(ServerConstants.ACTION_PACKAGE_ADDED)
                                            .putExtra(Intent.EXTRA_PACKAGE_NAME, SystemService
                                                    .getPackageInfoNoThrow(packageName, 0, 0)));
                                }
                                break;
                            case Intent.ACTION_PACKAGE_REPLACED:
                                if (ServerConstants.APPLICATION_ID.equals(packageName)) {
                                    mServer.broadcastIntentDelayed(broadcastIntent -> broadcastIntent
                                            .setAction(ServerConstants.ACTION_MY_PACKAGE_REPLACED)
                                    );
                                }
                                break;
                            case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                                mInstalledPackages.remove(packageName);
                                if (ServerConstants.APPLICATION_ID.equals(packageName)) {
                                    mServer.cleanerService.exit();
                                }
                                break;
                        }
                        if (ordered) {
                            CleanerHooksClient.whileAlive(service -> {
                                try {
                                    service.finishReceiver(this, resultCode, data, extras, false,
                                            intent.getFlags());
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                },
                intentFilter, -1, 0
        );
    }
}
