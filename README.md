# pdf-tools-api
PDF Tools API

## Create a self-signed certificate with OpenSSL
- create the key and cert`openssl req -x509 -newkey rsa:4096 -keyout cert-key.pem -out cert.pem -days 365 -nodes`
- create pkcs12 file `openssl pkcs12 -export -out keyStore.p12 -inkey cert-key.pem -in cert.pem`