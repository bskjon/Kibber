package no.iktdev.kibber

import no.iktdev.tibber.TibberHomesQuery

interface TibberCalls {
    abstract suspend fun homes(): List<TibberHomesQuery.Home>
    abstract suspend fun homesWithLiveReader(): List<TibberHomesQuery.Home>
    abstract var registeredLiveListener: TibberListener?

    abstract suspend fun readLiveWattage(homeId: String)


}