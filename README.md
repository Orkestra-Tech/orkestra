Orchestra
=========

Quick start
-----------

- Install minikube: `brew cask install minikube`
- minikube need virtualbox: `brew cask install virtualbox`
- Start minikube: `minikube start`
- You can view the dashboard and confirm that minikube has started: `minikube dashboard`
- Create credentials on minikube:
  The `secrets` folder is not committed to git, so ask Joan to sent it to you. We'll need to find a better solution.
  `sh -c 'cd orchestration/secrets && chmod +x create-secrets.sh && ./create-secrets.sh'`
- Deploy latest orchestration version: `kubectl apply -f orchestration/orchestration-minikube.yml`
- You can see the Pod creating on the dashboard (`minikube dashboard`)
- Connect on orchestra UI: `http://<Same IP as the dashboard>:31010`