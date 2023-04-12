package eu.pkgsoftware.babybuddywidgets.history

import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.VisibilityCheck
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListItem
import eu.pkgsoftware.babybuddywidgets.logic.EndAwareContinuousListIntegrator
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.EVENTS
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker.TimelineListener
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker.TimelineObserver
import java.util.*

class ChildEventHistoryLoader(
    private val fragment: BaseFragment,
    private val container: LinearLayout,
    private val childId: Int,
    private val visibilityCheck: VisibilityCheck,
    private val progressBar: ProgressBar
) {
    private var timelineObserver: TimelineObserver? = null
    private val timeEntryLookup = mutableMapOf<ContinuousListItem, TimeEntry>()
    private val listIntegrator = EndAwareContinuousListIntegrator()
    private val currentList = mutableListOf<TimelineEntry>()
    private val removedViews = mutableListOf<TimelineEntry>()

    fun createTimelineObserver(stateTracker: ChildrenStateTracker) {
        close()
        timelineObserver = stateTracker.TimelineObserver(childId, object : TimelineListener {
            override fun sleepRecordsObtained(offset: Int, totalCount: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, totalCount, ACTIVITIES.SLEEP, entries)
            }

            override fun tummyTimeRecordsObtained(offset: Int, totalCount: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, totalCount, ACTIVITIES.TUMMY_TIME, entries)
            }

            override fun feedingRecordsObtained(offset: Int, totalCount: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, totalCount, ACTIVITIES.FEEDING, entries)
            }

            override fun changeRecordsObtained(offset: Int, totalCount: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, totalCount, EVENTS.CHANGE, entries)
            }
        })
    }

    private fun newTimelineEntry(e: TimeEntry?): TimelineEntry {
        val result = if (removedViews.size > 0) {
            removedViews.removeLast()
        } else {
            TimelineEntry(fragment, e)
        };
        result.timeEntry = e
        result.setModifiedCallback { timelineObserver?.forceUpdate() }
        container.addView(result.view)
        currentList.add(result)
        return result
    }

    private fun timeEntryToContinuousListItem(e: TimeEntry): ContinuousListItem {
        val result = ContinuousListItem(
            -e.start.time,
            e.type,
            e.typeId.toString()
        )
        timeEntryLookup[result] = e
        return result
    }

    private fun addTimelineItems(offset: Int, totalCount: Int, type: String, _entries: Array<TimeEntry>) {
        val to = timelineObserver ?: return

        listIntegrator.updateItemsWithCount(
            offset,
            totalCount,
            type,
            _entries.map { timeEntryToContinuousListItem(it) }.toTypedArray()
        )

        val newOffset = listIntegrator.suggestClassQueryOffset(type)
        to.queryOffsets[type] = newOffset
        if (newOffset != offset) {
            updateTop()
            to.forceUpdate()
        }

        updateTimelineList()
    }

    private fun updateTimelineList() {
        val items = listIntegrator.items
        var i = 0
        val visibleCount = listIntegrator.computeValidCount()

        items.forEach {
            val entry = timeEntryLookup[it]
            val listItem: TimelineEntry =
                if (i < currentList.size) {
                    currentList[i]
                } else {
                    newTimelineEntry(entry)
                };
            if (it.dirty) {
                listItem.timeEntry = null
            } else {
                listItem.timeEntry = entry
            }
            listItem.view.visibility = if (i < visibleCount) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            i++
        }
        progressBar.visibility = if (visibleCount == items.size) {
                View.GONE
            } else {
                View.VISIBLE
            }
        while (currentList.size > items.size) {
            val removed = currentList.removeLast()
            if (removedViews.size < 128) {
                removedViews.add(removed)
            }
            container.removeView(removed.view)
        }
    }

    fun close() {
        timelineObserver?.close()
        timelineObserver = null
        listIntegrator.clear()
        removedViews.clear()
        updateTimelineList()
        timeEntryLookup.clear()
    }

    fun updateTop() {
        var i = 0
        listIntegrator.top = null
        for (item in currentList) {
            if (visibilityCheck.checkPartiallyVisible(item.view)) {
                listIntegrator.top = listIntegrator.items[i]
                break
            }
            i++
        }

        val to = timelineObserver ?: return

        for (clsName in ACTIVITIES.ALL) {
            to.queryOffsets[clsName] = listIntegrator.suggestClassQueryOffset(clsName)
        }
        for (clsName in EVENTS.ALL) {
            to.queryOffsets[clsName] = listIntegrator.suggestClassQueryOffset(clsName)
        }
    }

    fun forceRefresh() {
        timelineObserver?.forceUpdate()
    }
}