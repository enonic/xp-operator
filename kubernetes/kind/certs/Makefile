create-certs:
	openssl genrsa -des3 -passout pass:mysecret -out ca.key 4096
	openssl req -x509 -new -passin pass:mysecret -nodes -key ca.key -sha256 -days 3650 -out ca.crt -subj '/CN=myauthority'
	openssl genrsa -out tls.key 4096
	openssl req -new -sha256 -key tls.key -out tls.csr -subj "/CN=xp-operator"
	openssl x509 -req -passin pass:mysecret -in tls.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out tls.crt -days 3650 -sha256 -extfile <(printf "subjectAltName=DNS:operator.default.svc")
	openssl x509 -in tls.crt -text -noout
	rm ca.srl ca.key tls.csr

base-cert:
	@cat ca.crt | base64 -w0
