package tech.orkestra.github

/**
  * A Git reference
  *
  * @param name The name of the branch or commit id
  */
case class GitRef(name: String) extends AnyVal
