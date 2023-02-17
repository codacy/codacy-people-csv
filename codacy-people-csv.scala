//> using lib "com.lihaoyi::requests::0.8.0"
//> using lib "com.lihaoyi::ujson::2.0.0"
//> using lib "com.lihaoyi::os-lib::0.9.0"
//> using lib "com.lihaoyi::mainargs::0.3.0"

import java.{util => ju}
import java.time.Instant

@mainargs.main
def csv(provider: String, organization: String, token: String) = {
  def getPage(cursor: Option[String]) = {
    val data = ujson.read(
      requests.get.stream(
        s"https://app.codacy.com/api/v3/organizations/$provider/$organization/people",
        params = cursor.map("cursor" -> _).toSeq,
        headers = Seq(
          "api-token" -> token
        )
      )
    )
    val nextCursor =
      try { Some(data("pagination")("cursor").str) }
      catch { case _: ju.NoSuchElementException => None }

    data("data").arr -> nextCursor
  }

  def getAll() = {
    def loop(cursor: Option[String]): (collection.Seq[ujson.Value]) = {
      getPage(cursor) match {
        case (data, Some(newCursor)) => data ++ loop(Some(newCursor))
        case (data, None)            => data
      }
    }

    loop(cursor = None)
  }

  val lines = getAll().map { json =>
    val obj = json.obj
    def get(name: String) =
      obj.get(name).map(email => s"\"${email.str}\"").getOrElse("")
    s"${get("email")},${get("name")},${get("lastAnalysis")},${get("lastLogin")}"
  }

  val content = ("email,name,lastAnalysis,lastLogin" +: lines).mkString("\n")

  val now = Instant.now()
  val file = os.pwd / s"$organization-people-$now.csv"
  os.write.over(file, content)
  println(s"Saved the list of people in $organization to $file")
}

@main
def run(args: String*) = mainargs.ParserForMethods(this).runOrExit(args)
