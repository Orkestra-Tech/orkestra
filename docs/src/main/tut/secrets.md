---
layout: docs
title:  "Secrets"
position: 7
---

# Secrets

Secrets in Orkestra relies on the [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/). As
this is already implemented we thought there is no need to add an Orkestra specific implementation.

## Secrets as Environment Variables

On Kubernetes we most commonly use secrets as environment variables.  
First we need to create the secret on Kubernetes, then pass the secret to the container, all of that is well explained
in the documentation of [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/).  
Then we can access the secret safely with the `Secrets` utility object which will make sure the secrets are not saved in
the logs:
```tut:silent
import tech.orkestra.utils.Secrets

val slackToken = Secrets.get("SLACK_TOKEN").getOrElse("not set")
println(s"Look at my secret Slack token: $slackToken!")
```
This will print `Look at my secret Slack token: **********!` in the logs.

Attention: Do not use the Scala `sys.env.get()` or the Java `System.getEnv()` as these will not prevent the secret to be
logged.

## Secrets as Files

You might want to use for example an SSH key file as a Secret. We usually mount the secret in a file under
`/opt/docker/secrets/ssh` and then access it via this path:
```scala
sh(s"ansible-playbook some-playbook.yml --private-key /opt/docker/secrets/ssh")
```
