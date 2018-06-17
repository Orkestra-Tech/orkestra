---
layout: docs
title:  "Config"
position: 2
---

# Config

- `ORKESTRA_KUBE_URI`: The URI of the Kubernetes API. Required.
- `ORKESTRA_ELASTICSEARCH_URI`: The URI of Elasticsearch for the backend storage. Required.
- `ORKESTRA_BIND_PORT`: Port to bind the API and UI on. Default: 8080.
- `ORKESTRA_NAMESPACE`: Orkestra needs to know in which namespace it lives in. Required.  
  The easiest way is giving it via the downward API:

  ```yaml
  - name: ORKESTRA_NAMESPACE
    valueFrom:
      fieldRef:
        fieldPath: metadata.namespace
  ```

- `ORKESTRA_POD_NAME`: Orkestra needs to know the name of pod it runs in. Required.  
  The easiest way is giving it via the downward API:

  ```yaml
  - name: ORKESTRA_POD_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
  ```

- `ORKESTRA_WORKSPACE`: If you'd like you can change the default workspace where the jobs will run in. Required.
  Default `/opt/docker/workspace`.
- `ORKESTRA_BASEPATH`: If the UI is accessed form a sub path we need to make Orkestra aware of it. Optional.

## Deployment config

The deployment can be done via Kubernetes Deployments.

First we create a namespace where we will be deploying everything:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: orkestra
```

Then we need to have Elasticsearch running so that Orkestra can store and query the data related to the runs of jobs
(logs, times, parameters...). The following one is deploying a single node which is fine for dev purpose but you might
want to have a cluster of at least 3 nodes to be HA and failure resilient:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch
  namespace: orkestra
spec:
  selector:
    app: elasticsearch
  clusterIP: None
  ports:
  - port: 9200
    targetPort: 9200

---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch-internal
  namespace: orkestra
spec:
  selector:
    app: elasticsearch
  clusterIP: None
  ports:
  - port: 9300
    targetPort: 9300

---
apiVersion: apps/v1beta2
kind: StatefulSet
metadata:
  name: elasticsearch
  namespace: orkestra
spec:
  selector:
    matchLabels:
      app: elasticsearch
  serviceName: elasticsearch-internal
  replicas: 1
  template:
    metadata:
      labels:
        app: elasticsearch
    spec:
      initContainers:
      - name: init-sysctl
        image: busybox:1.27.2
        command:
        - sysctl
        - -w
        - vm.max_map_count=262144
        securityContext:
          privileged: true
      containers:
      - name: elasticsearch
        image: docker.elastic.co/elasticsearch/elasticsearch-oss:6.1.1
        env:
        - name: cluster.name
          value: orkestra
        - name: node.name
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: discovery.zen.ping.unicast.hosts
          value: elasticsearch-internal
        volumeMounts:
        - name: data
          mountPath: /usr/share/elasticsearch/data
      volumes:
      - name: data
        emptyDir: {}
```

Now we can deploy Orkestra:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: orkestra
  namespace: orkestra
spec:
  selector:
    app: orkestra
  ports:
  - name: http
    port: 80
    targetPort: 8080
  - name: github
    port: 81
    targetPort: 8081

---
apiVersion: apps/v1beta2
kind: Deployment
metadata:
  name: orkestra
  namespace: orkestra
spec:
  replicas: 1
  selector:
    matchLabels:
      app: orkestra
  template:
    metadata:
      labels:
        app: orkestra
    spec:
      containers:
      - name: orkestra
        image: orkestra:0.1.0-SNAPSHOT
        imagePullPolicy: IfNotPresent
        env:
        - name: ORKESTRA_KUBE_URI
          value: https://kubernetes.default
        - name: ORKESTRA_ELASTICSEARCH_URI
          value: elasticsearch://elasticsearch:9200
        - name: ORKESTRA_BASEPATH
          value: /api/v1/namespaces/orkestra/services/orkestra:http/proxy
        - name: ORKESTRA_POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: ORKESTRA_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
```
