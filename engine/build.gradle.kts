plugins { `java-library` }

// Один набор исходников движка; старый Compose-клиент пока собирается независимо.
sourceSets.main { java.setSrcDirs(listOf("../composeApp/src/jvmMain/java")) }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
