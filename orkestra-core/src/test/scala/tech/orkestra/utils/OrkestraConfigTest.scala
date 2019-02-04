package tech.orkestra.utils

import com.sksamuel.elastic4s.http.ElasticProperties
import org.http4s.Uri
import tech.orkestra.OrkestraConfig
import tech.orkestra.model.{JobId, RunId, RunInfo}

trait OrkestraConfigTest {
  implicit val orkestraConfig: OrkestraConfig =
    OrkestraConfig(
      elasticsearchProperties = ElasticProperties("http://elasticsearch:9200"),
      runInfoMaybe = Option(RunInfo(JobId("someJob"), RunId.random())),
      kubeUri = Uri.unsafeFromString(s"http://localhost"),
      namespace = "someNamespace",
      podName = "somePod"
    )
}
