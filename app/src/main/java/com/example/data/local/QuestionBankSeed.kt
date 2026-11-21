package com.example.data.local

object QuestionBankSeed {

    val units: List<UnitEntity> = listOf(
        UnitEntity(
            unitId = "unit_1",
            orderIndex = 1,
            title = "Actualités BF & AES",
            subtitle = "Réforme territoriale, AES, FESPACO 2025 et grands chantiers",
            badgeLabel = "ACTU 2024-2026",
            colorKey = "emerald"
        ),
        UnitEntity(
            unitId = "unit_2",
            orderIndex = 2,
            title = "Histoire, Géo & Institutions",
            subtitle = "Burkina Faso, fleuves, UNESCO, ONU, UA & ZLECAf",
            badgeLabel = "HISTOIRE-GÉO",
            colorKey = "cobalt"
        ),
        UnitEntity(
            unitId = "unit_3",
            orderIndex = 3,
            title = "Littérature & Langue",
            subtitle = "Auteurs burkinabè, classiques africains, grammaire & style",
            badgeLabel = "LETTRES",
            colorKey = "violet"
        ),
        UnitEntity(
            unitId = "unit_4",
            orderIndex = 4,
            title = "Sciences & Environnement",
            subtitle = "Biologie cellulaire, santé, physique, optique & chimie",
            badgeLabel = "SCIENCES",
            colorKey = "teal"
        ),
        UnitEntity(
            unitId = "unit_5",
            orderIndex = 5,
            title = "Économie, Droit & Logique",
            subtitle = "Finance, OHADA, Constitution, suites & probabilités",
            badgeLabel = "ÉCO & LOGIQUE",
            colorKey = "amber"
        )
    )

    val levels: List<LevelEntity> = listOf(
        // Unit 1
        LevelEntity("u1_l1", "unit_1", 1, "Réforme territoriale", "17 régions et 47 provinces", 30, false, 6),
        LevelEntity("u1_l2", "unit_1", 2, "Alliance des États du Sahel", "Charte, hymne et retrait CEDEAO", 35, false, 6),
        LevelEntity("u1_l3", "unit_1", 3, "FESPACO 2025 & Culture", "Palmarès, SIAO et distinctions", 40, false, 6),
        LevelEntity("u1_l4", "unit_1", 4, "Défi Unité 1 : Actualités", "Faso Mêbo, Arzeka & Monde", 60, true, 6),

        // Unit 2
        LevelEntity("u2_l1", "unit_2", 1, "Histoire du Burkina Faso", "Dates clés et symboles nationaux", 30, false, 6),
        LevelEntity("u2_l2", "unit_2", 2, "Géographie & Patrimoine", "Fleuves, reliefs et sites UNESCO", 35, false, 6),
        LevelEntity("u2_l3", "unit_2", 3, "Organisations mondiales", "ONU, OUA/UA, CILSS & ZLECAf", 40, false, 6),
        LevelEntity("u2_l4", "unit_2", 4, "Défi Unité 2 : Institutions", "Constitution, BRICS, UE & Monde", 60, true, 6),

        // Unit 3
        LevelEntity("u3_l1", "unit_3", 1, "Littérature Burkinabè", "Nazi Boni, Ki-Zerbo, Pacéré...", 30, false, 6),
        LevelEntity("u3_l2", "unit_3", 2, "Classiques Africains", "Négritude, Kourouma, Sembène...", 35, false, 6),
        LevelEntity("u3_l3", "unit_3", 3, "Littérature & Figures de style", "Zola, Camus, Baudelaire, Hugo", 40, false, 6),
        LevelEntity("u3_l4", "unit_3", 4, "Défi Unité 3 : Langue", "Grammaire, orthographe & lexique", 60, true, 6),

        // Unit 4
        LevelEntity("u4_l1", "unit_4", 1, "Biologie & Génétique", "ATP, mitose, méiose et ADN", 30, false, 6),
        LevelEntity("u4_l2", "unit_4", 2, "Corps Humain & Santé", "Organes, hormones, immunité", 35, false, 6),
        LevelEntity("u4_l3", "unit_4", 3, "Physique & Optique", "Loi d'Ohm, puissance, lentilles", 40, false, 6),
        LevelEntity("u4_l4", "unit_4", 4, "Défi Unité 4 : Chimie & SVT", "Réactions, ions et découvertes", 60, true, 6),

        // Unit 5
        LevelEntity("u5_l1", "unit_5", 1, "Économie & Finance", "Bilan, VAN/TRI, marchés & inflation", 30, false, 6),
        LevelEntity("u5_l2", "unit_5", 2, "Droit & OHADA", "SARL, travail, Constitution", 35, false, 6),
        LevelEntity("u5_l3", "unit_5", 3, "Suites & Logique", "Séries numériques et alphanumériques", 40, false, 6),
        LevelEntity("u5_l4", "unit_5", 4, "Grand Défi : Examen Blanc", "Maths, probabilités et synthèse", 75, true, 6)
    )

    fun allQuestions(): List<QuizQuestionEntity> =
        QuestionsPart1.unit1Questions +
            QuestionsPart1.unit2Questions +
            QuestionsPart2.unit3Questions +
            QuestionsPart2.unit4Questions +
            QuestionsPart2.unit5Questions
}
