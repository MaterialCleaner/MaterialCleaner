package me.gm.cleaner.home.ui;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigator;

import me.gm.cleaner.R;
import me.gm.cleaner.home.StaticScanner;

// https://stackoverflow.com/questions/58394262/how-to-pass-generic-data-types-with-safe-args
public class HomeToTrashNavigator {
    private final HomeFragmentDirections.HomeToTrashAction mDirection;
    private Navigator.Extras mExtras;

    public HomeToTrashNavigator(StaticScanner info) {
        mDirection = HomeFragmentDirections.homeToTrashAction(
                info.getTitle(), info.getIcon(), info.getViewModelClass(), info.getServiceClass());
    }

    public HomeToTrashNavigator(StaticScanner info, Navigator.Extras extras) {
        mDirection = HomeFragmentDirections.homeToTrashAction(
                info.getTitle(), info.getIcon(), info.getViewModelClass(), info.getServiceClass());
        mExtras = extras;
    }

    public void navigate(@NonNull NavController navController) {
        var currentDestination = navController.getCurrentDestination();
        if (currentDestination == null || R.id.home_fragment != currentDestination.getId()) {
            return;
        }
        if (mExtras == null) {
            navController.navigate(mDirection);
        } else {
            navController.navigate(mDirection, mExtras);
        }
    }
}
