CREATE TABLE PROPERTIES (
    ID              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    APPLICATION     VARCHAR2(128)   NOT NULL,
    PROFILE         VARCHAR2(128)   NOT NULL,
    LABEL           VARCHAR2(128)   NOT NULL,
    PROP_KEY        VARCHAR2(256)   NOT NULL,
    PROP_VALUE      VARCHAR2(4000),
    CREATED_AT      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- gateway-service 기본 설정
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

COMMIT;
