def toto: PartialFunction[String, Unit] = {
  case "toto" => ()
}

toto("tutu")