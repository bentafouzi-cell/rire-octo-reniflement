# On utilise une image Python légère
FROM python:3.9-slim

# On définit le dossier de travail dans le conteneur
WORKDIR /app

# On copie notre script dans le conteneur
COPY app.py .

# On dit au conteneur d'exécuter le script au démarrage
CMD ["python", "app.py"]
