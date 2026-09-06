FROM node:22-alpine
WORKDIR /bot
COPY package.json .
RUN npm install --omit=dev --no-audit --no-fund
COPY index.js .
CMD ["node", "index.js"]
