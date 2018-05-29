package com.goyeau.orchestra.utils

object Secrets {
  private var secrets = Seq.empty[String]

  private[orchestra] def sanitize(string: String): String =
    secrets.foldLeft(string) {
      case (line, secret) =>
        line.replace(secret, "**********")
    }

  def apply(name: String): String =
    get(name: String).getOrElse(throw new IllegalArgumentException(s"Missing secret $name"))

  def get(name: String): Option[String] =
    sys.env.get(name).map { secret =>
      secrets = secrets :+ secret
      secret
    }
}
