export const environment = {
  production: false,
  // Frontend talks ONLY to the API Gateway -- never to an individual
  // microservice directly. The Gateway resolves every downstream route
  // dynamically via Eureka (see api-gateway-service's RouteConfig).
  apiBaseUrl: 'http://localhost:8080',
};
