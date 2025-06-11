A server providing access to a database through an API. Specifically built for a project. Not intented for genreal use.

Authentication is done through HMACSHA256 signed tokens.<br>
Since this server is inteded for use without a domain, encryption is done through the rsaRelay endpoint.
The rsaRelay endpoint accespts a request that conain another inner request in its body, which is encrypted using
the servers public RSA key. the inner request conains an AES key that is used to encrypt the inner request of the response.




