//> using scala "3.1.2"
//> using lib "com.lihaoyi::os-lib:0.8.0"
import scala.util.Properties

val platformSuffix: String = {
  val os =
    if (Properties.isWin) "pc-win32"
    else if (Properties.isLinux) "pc-linux"
    else if (Properties.isMac) "apple-darwin"
    else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
  os
}
val artifactsPath = os.Path("artifacts", os.pwd)
val destPath =
  if (Properties.isWin) artifactsPath / s"codacy-people-csv-$platformSuffix.exe"
  else artifactsPath / s"codacy-people-csv-$platformSuffix"
val scalaCLILauncher =
  if (Properties.isWin) "scala-cli.bat" else "scala-cli"

os.makeDir(artifactsPath)
os.proc(
  scalaCLILauncher,
  "package",
  ".",
  "-o",
  destPath,
  "--native-image",
  "--graalvm-args",
  "--enable-url-protocols=https"
).call(cwd = os.pwd)
  .out
  .text()
  .trim
