group = "com.nuix.proserv"
version = "1.0-SNAPSHOT"

val wixPath = "C:\\Program Files (x86)\\WiX Toolset v3.11\\bin\\"
val srcPath = "src\\main\\wix\\"
val objPath = "build\\classes\\"
val msiPath = "build\\installer\\"
val pdbPath = "build\\pdbs\\"

val fragments = arrayOf("Nuix T3K Analysis Installer",
    "application", "data", "help", "scripts")

val candle = wixPath + "candle.exe"
val light = wixPath + "light.exe"

val utilExtension = "WixUtilExtension"
val uiExtension = "WixUIExtension"

task<Exec>("linkInstallerSources") {
    group = "Installer"
    val sourceFragments = fragments.map { "\"$srcPath$it.wxs\"" }
    val candleCommand = "\"$candle\" -ext $utilExtension -arch x64 -o $objPath ${sourceFragments.joinToString(separator=" ")}"
    commandLine(candleCommand)
}

task<Exec>("buildMsiFile") {
    group = "Installer"
    dependsOn(":linkInstallerSources")
    val objFragments = fragments.map { "\"$objPath$it.wixobj\"" }
    val lightCommand = "\"$light\" -ext $uiExtension -ext $utilExtension -pdbout \"$pdbPath${fragments[0]}.wixpdb\" -o \"$msiPath${fragments[0]}.msi\" ${objFragments.joinToString(separator=" ")}"
    commandLine(lightCommand)
}
