Onboarding a new service (e.g. orderService)

Step 1 — pom.xml:
<dependency>
<groupId>com.bhagwat.scm</groupId>
<artifactId>api-observability</artifactId>
<version>1.0.0-SNAPSHOT</version>
</dependency>

Step 2 — Main class:
@SpringBootApplication
@EnableObservability
public class OrderServiceApplication { }

Step 3 — application.properties:
logging.config=classpath:logback-observability.xml

Step 4 — shared-library/observability/prometheus.yml (uncomment the block):
- job_name: order-service
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: [host.docker.internal:8085]
      relabel_configs:
    - target_label: service_name
      replacement: orderService
      Then: curl -X POST http://localhost:9090/-/reload

Step 5 — Drop dashboard — copy _TEMPLATE-dashboard.json → orderService-dashboard.json, replace all <<serviceName>> with orderService. Grafana picks it up in 10 seconds automatically.

