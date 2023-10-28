package com.example.crossclock.data

class Repository(
    private val worldClockDao: WorldClockDao
) {
    val allWorldClock = worldClockDao.loadAllWorldClock()
    suspend fun insertWorldClock(worldClock: WorldClock){
        worldClockDao.insertWorldClock(worldClock)
    }

    suspend fun updateWorldClock(worldClock: WorldClock){
        worldClockDao.updateWorldClock(worldClock)
    }

    suspend fun deleteWorldClock(worldClock: WorldClock){
        worldClockDao.deleteWorldClock(worldClock)
    }

    suspend fun deleteWorldClockByName(cityName: String) {
        worldClockDao.deleteWorldClockByCityName(cityName)
    }

    fun existsWorldClock(cityName: String) = worldClockDao.existsWorldClock(cityName)

}