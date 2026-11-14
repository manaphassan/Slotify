package my.slotify.app.feature.logistics

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.slotify.app.core.routing.OsrmRouteResponse
import my.slotify.app.core.routing.OsrmRoutingService

data class LogisticsUiState(
    val pois: List<PoiItem> = emptyList(),
    val selectedRoute: RouteSelection = RouteSelection.PARENT_PICKUP,
    /** Live OSRM route, or null when offline / not yet loaded. */
    val route: OsrmRouteResponse? = null,
    val isRouteLoading: Boolean = false
)

/**
 * Owns POIs, the selected route and OSRM routing so they survive rotation and are not
 * re-fetched on every recomposition. Map rendering, GPS, simulation and TTS stay in the UI.
 */
class LogisticsViewModel(
    private val appContext: Context,
    private val savedState: SavedStateHandle,
    private val osrm: OsrmRoutingService = OsrmRoutingService()
) : ViewModel() {

    private val pois = MutableStateFlow<List<PoiItem>>(emptyList())
    private val selectedRoute = savedState.getStateFlow(KEY_ROUTE, RouteSelection.PARENT_PICKUP.name)
    private val refreshTick = MutableStateFlow(0)
    private val route = MutableStateFlow<OsrmRouteResponse?>(null)
    private val loading = MutableStateFlow(false)

    val uiState: StateFlow<LogisticsUiState> =
        combine(pois, selectedRoute, route, loading) { p, r, rt, l ->
            LogisticsUiState(p, RouteSelection.valueOf(r), rt, l)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogisticsUiState())

    init {
        viewModelScope.launch {
            pois.value = withContext(Dispatchers.IO) { PoiStorage.loadPois(appContext) }
            // Re-route whenever the route, the waypoints or a manual refresh changes;
            // collectLatest cancels an in-flight request that has become stale.
            combine(selectedRoute, pois, refreshTick) { r, p, _ -> resolveRouteWaypoints(RouteSelection.valueOf(r), p) }
                .collectLatest { waypoints ->
                    loading.value = true
                    route.value =
                        if (waypoints.size >= 2) osrm.getDrivingRoute(osrmCoordinateString(waypoints)) else null
                    loading.value = false
                }
        }
    }

    fun selectRoute(selection: RouteSelection) {
        savedState[KEY_ROUTE] = selection.name
    }

    fun refreshRoute() {
        refreshTick.value++
    }

    /** Replaces the POI list and persists it. */
    fun setPois(updated: List<PoiItem>) {
        pois.value = updated
        viewModelScope.launch(Dispatchers.IO) { PoiStorage.savePois(appContext, updated) }
    }

    fun resetPois() {
        viewModelScope.launch {
            pois.value = withContext(Dispatchers.IO) { PoiStorage.resetToDefaults(appContext) }
        }
    }

    private companion object {
        const val KEY_ROUTE = "selected_route"
    }
}
