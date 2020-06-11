apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}
  labels:
    app: {{ template "eth2signer.name" . }}
    chart: {{ template "eth2signer.chart" . }}
    release: {{ .Release.Name }}
    heritage: {{ .Release.Service }}

data:
{{ toYaml .Values.config | indent 2 }}



