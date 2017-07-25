package com.drivetribe.orchestration

import com.goyeau.orchestra.Container
import com.goyeau.orchestra._

object MySqlContainer extends Container("mysql", "mysql:5.7.18", tty = true, Seq("cat")) {

  def dump(source: Environment, destination: Environment, dbName: String = "user_identity", params: String = "") = {
    mysql(destination, "", s"CREATE DATABASE IF NOT EXISTS $dbName")
    sh(
      s"""mysqldump \\
         |  -h $source-aurora-ro.drivetribe.com \\
         |  -u${System.getenv("AURORA_USERNAME")} -p${System.getenv("AURORA_PASSWORD")} \\
         |  --single-transaction \\
         |  --compress $params $dbName | \\
         |  mysql \\
         |    -h $destination-aurora.drivetribe.com \\
         |    -u${System.getenv("AURORA_USERNAME")} -p${System.getenv("AURORA_PASSWORD")} $dbName""".stripMargin,
      this
    )
  }

  def copySubset(source: Environment, destination: Environment, dbName: String = "user_identity") {
    mysql(destination, "", "CREATE DATABASE IF NOT EXISTS $dbName")
    dump(source, destination, dbName, "--tables devices magic_tokens mimics takeovers users")
    dump(source, destination, dbName, "--tables credentials --where 'registered = 1'")
  }

  def mysql(environment: Environment, dbName: String, command: String) =
    sh(
      s"""mysql \\
         |  -h $environment-aurora.drivetribe.com -u${System.getenv("AURORA_USERNAME")} \\
         |  -p${System.getenv("AURORA_PASSWORD")} \\
         |  -e '$command' $dbName""".stripMargin,
      this
    )

}
