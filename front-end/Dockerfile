FROM node:alpine3.23 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

FROM nginx:stable-trixie-perl
# Remove a configuração padrão
RUN rm -rf /usr/share/nginx/html/* && rm -f /etc/nginx/conf.d/default.conf
# Copia a configuração customizada
COPY nginx/default.conf /etc/nginx/conf.d/
COPY --from=builder /app/dist/jardim-inteligente/browser /usr/share/nginx/html
EXPOSE 80
#EXPOSE 4200
CMD ["nginx", "-g", "daemon off;"]
