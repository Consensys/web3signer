apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}
  labels:
    app: {{ template "web3signer.name" . }}
    chart: {{ template "web3signer.chart" . }}
    release: {{ .Release.Name }}
    heritage: {{ .Release.Service }}

data:
{{ toYaml .Values.config | indent 2 }}



