INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE)
VALUES ('gateway-service', 'default', 'main', 'jwt.secret',
        'YourProductionSecretKeyMustBeAtLeast64BytesLongForHS512Algorithm!!ChangeMe');

INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE)
VALUES ('gateway-service', 'default', 'main', 'rate-limit.requests-per-second', '50');

INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE)
VALUES ('gateway-service', 'default', 'main', 'rate-limit.burst-capacity', '100');

INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE)
VALUES ('gateway-service', 'default', 'main', 'rate-limit.timeout-millis', '500');

INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, PROP_VALUE)
VALUES ('gateway-service', 'default', 'main',
        'spring.cloud.gateway.server.webflux.trusted-proxies', '10.0.0.0/8,172.16.0.0/12');
