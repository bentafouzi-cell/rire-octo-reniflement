import sys
import time

def analyse_medicale():
    print("🏥 Démarrage de l'analyse médicale simulée...")
    print("🔍 Chargement des données anonymisées...")
    time.sleep(2)  # Simulation d'un traitement
    print("🧠 Analyse des images en cours...")
    time.sleep(2)  # Simulation d'un traitement
    print("✅ Résultat : Analyse terminée avec succès. Aucune anomalie détectée.")
    return 0

if __name__ == "__main__":
   print("🔥 Trigger automatique réussi ! Le pipeline s'est lancé tout seul.") 
  sys.exit(analyse_medicale()) 
