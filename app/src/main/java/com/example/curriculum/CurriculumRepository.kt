package com.example.curriculum

import com.example.data.SessionProgressEntity

object CurriculumRepository {

    private val modulesList: List<ReadingModule> by lazy {
        Ks1EnglishCurriculum.modules +
            Ks1FilipinoCurriculum.modules +
            Ks2BasicCurriculum.modules +
            Ks2PlusCurriculum.modules
    }

    private val modulesById: Map<String, ReadingModule> by lazy {
        modulesList.associateBy { it.id }
    }

    fun getModulesForTrack(track: ReadingTrack): List<ReadingModule> {
        return when (track) {
            ReadingTrack.KS1_ENGLISH -> Ks1EnglishCurriculum.modules
            ReadingTrack.KS1_FILIPINO -> Ks1FilipinoCurriculum.modules
            ReadingTrack.KS2_BASIC -> Ks2BasicCurriculum.modules
            ReadingTrack.KS2_PLUS -> Ks2PlusCurriculum.modules
        }
    }

    fun getModuleById(moduleId: String): ReadingModule? = modulesById[moduleId]

    fun getAllModules(): List<ReadingModule> = modulesList

    /**
     * Resolves the best module to resume for a returning learner:
     * 1) Next uncompleted module after their lastModuleId on preferred track
     * 2) First uncompleted module on preferred track
     * 3) Their lastModuleId or first module of the track
     */
    fun resolveResumeModule(
        track: ReadingTrack,
        lastModuleId: String?,
        progressList: List<SessionProgressEntity>,
    ): ReadingModule {
        val trackModules = getModulesForTrack(track)
        val completedIds = progressList
            .filter { it.attemptsCount > 0 }
            .map { it.moduleId }
            .toSet()

        if (lastModuleId != null) {
            val lastIdx = trackModules.indexOfFirst { it.id == lastModuleId }
            if (lastIdx >= 0) {
                val nextUncompleted = trackModules.drop(lastIdx + 1).firstOrNull { it.id !in completedIds }
                if (nextUncompleted != null) return nextUncompleted
            }
        }
        return trackModules.firstOrNull { it.id !in completedIds }
            ?: lastModuleId?.let { modulesById[it] }
            ?: trackModules.first()
    }
}
