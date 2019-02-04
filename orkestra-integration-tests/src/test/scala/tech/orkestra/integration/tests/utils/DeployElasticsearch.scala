//package tech.orkestra.integration.tests.utils
//
//import com.goyeau.kubernetes.client.{IntValue, KubernetesClient}
//import io.k8s.api.apps.v1.{StatefulSet, StatefulSetSpec}
//import io.k8s.api.core.v1._
//import io.k8s.apimachinery.pkg.apis.meta.v1.{LabelSelector, ObjectMeta}
//
//import tech.orkestra.utils.AkkaImplicits._
//
//object DeployElasticsearch {
//  val advertisedHostName = "elasticsearch"
//  val appElasticsearchLabel = Option(Map("app" -> "elasticsearch"))
//
//  val service = Service(
//    metadata = Option(ObjectMeta(name = Option(advertisedHostName))),
//    spec = Option(
//      ServiceSpec(
//        selector = appElasticsearchLabel,
//        ports = Option(Seq(ServicePort(port = 9200, targetPort = Option(IntValue(9200)))))
//      )
//    )
//  )
//
//  val internalService = Service(
//    metadata = Option(ObjectMeta(name = Option("elasticsearch-internal"))),
//    spec = Option(
//      ServiceSpec(
//        selector = appElasticsearchLabel,
//        clusterIP = Option("None"),
//        ports = Option(Seq(ServicePort(port = 9300, targetPort = Option(IntValue(9300)))))
//      )
//    )
//  )
//
//  val statefulSet = StatefulSet(
//    metadata = Option(ObjectMeta(name = service.metadata.get.name)),
//    spec = Option(
//      StatefulSetSpec(
//        selector = LabelSelector(matchLabels = appElasticsearchLabel),
//        serviceName = internalService.metadata.get.name.get,
//        replicas = Option(1),
//        template = PodTemplateSpec(
//          metadata = Option(ObjectMeta(labels = appElasticsearchLabel)),
//          spec = Option(
//            PodSpec(
//              initContainers = Option(
//                Seq(
//                  Container(
//                    name = "init-sysctl",
//                    image = Option("busybox:1.27.2"),
//                    command = Option(Seq("sysctl", "-w", "vm.max_map_count=262144")),
//                    securityContext = Option(SecurityContext(privileged = Option(true)))
//                  )
//                )
//              ),
//              containers = Seq(
//                Container(
//                  name = "elasticsearch",
//                  image = Option("docker.elastic.co/elasticsearch/elasticsearch-oss:6.1.1"),
//                  env = Option(
//                    Seq(
//                      EnvVar(name = "cluster.name", value = Option("orkestra")),
//                      EnvVar(
//                        name = "node.name",
//                        valueFrom =
//                          Option(EnvVarSource(fieldRef = Option(ObjectFieldSelector(fieldPath = "metadata.name"))))
//                      ),
//                      EnvVar(name = "discovery.zen.ping.unicast.hosts", value = internalService.metadata.get.name)
//                    )
//                  ),
//                  volumeMounts = Option(Seq(VolumeMount(name = "data", mountPath = "/usr/share/elasticsearch/data")))
//                )
//              ),
//              volumes = Option(Seq(Volume(name = "data", emptyDir = Option(EmptyDirVolumeSource()))))
//            )
//          )
//        )
//      )
//    )
//  )
//
//  def apply(kubernetesClient: KubernetesClient) =
//    for {
//      _ <- kubernetesClient.namespaces.createOrUpdate(Kubernetes.namespace)
//      _ <- kubernetesClient.services.namespace(Kubernetes.namespace.metadata.get.name.get).create(service)
//      _ <- kubernetesClient.services
//        .namespace(Kubernetes.namespace.metadata.get.name.get)
//        .create(internalService)
//      _ <- kubernetesClient.statefulSets
//        .namespace(Kubernetes.namespace.metadata.get.name.get)
//        .create(statefulSet)
//    } yield ()
//}
