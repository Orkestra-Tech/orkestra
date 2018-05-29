package com.goyeau.orchestra.utils

object Secrets {
  private var secrets = Seq.empty[String]

  private[orchestra] def sanitize(string: String): String =
    secrets.foldLeft(string) {
      case (line, secret) =>
        line.replace(secret, "**********")
    }

  def get(key: String): Option[String] =
    sys.env.get(key).map { secret =>
      secrets = secrets :+ secret
      secret
    }
}
