package me.gm.cleaner.widget.recyclerview

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.ListUpdateCallback

/**
 * Aware the update process of [androidx.recyclerview.widget.DiffUtil].
 */
abstract class UpdatingList<T>(oldList: List<T>, private val newList: List<T>) :
    ListUpdateCallback {
    private val _currentList: MutableList<T> = oldList.toMutableList()
    protected val currentList: List<T>
        get() = _currentList

    protected fun getPositionInNewList(position: Int): Int =
        newList.size - (_currentList.size - position)

    @CallSuper
    override fun onInserted(position: Int, count: Int) {
        val toIndex = getPositionInNewList(position)
        val insertedList = newList.subList(toIndex - count, toIndex)
        _currentList.addAll(position, insertedList)
        onInserted(insertedList)
    }

    protected open fun onInserted(list: List<T>) {}

    @CallSuper
    override fun onRemoved(position: Int, count: Int) {
        val removedList = ArrayList<T>(count)
        repeat(count) { removedList += _currentList.removeAt(position) }
        onRemoved(removedList)
    }

    protected open fun onRemoved(list: List<T>) {}

    @CallSuper
    override fun onMoved(fromPosition: Int, toPosition: Int) {
        val movedItem = _currentList.removeAt(fromPosition)
        _currentList.add(toPosition, movedItem)
        onMoved(listOf(movedItem))
    }

    protected open fun onMoved(list: List<T>) {}

    @CallSuper
    override fun onChanged(position: Int, count: Int, payload: Any?) {
        val origenList = ArrayList<T>(count)
        val changedList = ArrayList<T>(count)
        repeat(count) { index ->
            val curPosition = position + index
            origenList += _currentList[curPosition]
            _currentList[curPosition] = newList[getPositionInNewList(curPosition)]
            changedList += _currentList[curPosition]
        }
        onChanged(origenList, changedList)
    }

    protected open fun onChanged(originList: List<T>, changedList: List<T>) {}
}
