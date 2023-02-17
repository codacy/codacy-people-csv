//> using lib "com.lihaoyi::requests::0.8.0"
//> using lib "com.lihaoyi::ujson::2.0.0"
//> using lib "com.lihaoyi::os-lib::0.9.0"
//> using lib "com.lihaoyi::mainargs::0.3.0"

import java.{util => ju}
import java.time.Instant
import scala.collection.mutable

@mainargs.main
def csv(provider: String, organization: String, token: String) = {
  def getAll() = {
    val headers = mutable.Set.empty[String]

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

      val dataArray = data("data").arr
      dataArray.foreach { elem => headers ++= elem.obj.keySet }
      dataArray -> nextCursor
    }

    def loop(cursor: Option[String]): (collection.Seq[ujson.Value]) = {
      getPage(cursor) match {
        case (data, Some(newCursor)) => data ++ loop(Some(newCursor))
        case (data, None)            => data
      }
    }

    val result = loop(cursor = None)
    headers.toSeq.sorted -> result
  }

  val (headers, jsonObjects) = getAll()
  val lines = jsonObjects
    .map { json =>
      val obj = json.obj
      headers
        .map(name =>
          obj.get(name).map(value => s"\"${value.str}\"").getOrElse("")
        )
        .mkString(",")
    }

  val content = (headers.mkString(",") +: lines).mkString("\n")

  val now = Instant.now()
  val file = os.pwd / s"$organization-people-$now.csv"
  os.write.over(file, content)
  println(s"Saved the list of people in $organization to $file")
}

@main
def run(args: String*) = mainargs.ParserForMethods(this).runOrExit(args)
