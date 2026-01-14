# Fichiers Modifiés - SecureTeam Access

## Résumé des modifications

Ce document liste les fichiers principaux qui ont été modifiés pour implémenter le système d'approbation de requests avec interface admin/user.

### 1. **frontend/src/secureteam-app.js** (PRINCIPAL)
   - Ajout des propriétés pour gérer les requests et les commentaires d'approbation
   - Implémentation du localStorage pour persister les données
   - Navigation dynamique (Dashboard, Responses pour admin, Submit Request + My Requests pour users)
   - Formulaire de soumission de requests fonctionnel
   - Interface d'approbation/rejet pour l'admin
   - Affichage de l'état des requests pour les utilisateurs

   **Modifications clés:**
   - ✅ Login/MFA avec QR code généré par qrcode library
   - ✅ Système de requests avec persistance localStorage
   - ✅ Interface admin pour approuver/rejeter les requests
   - ✅ Interface utilisateur pour voir l'état de leurs requests
   - ✅ Tous les URL changés de `/secureteam-access/api/` à `/api/`

### 2. **frontend/src/mock-api.js** (API MOCK)
   - Implémentation d'une API mock pour tester sans backend réel
   - Endpoints implémentés: health, mfa/setup, mfa/verify, users, projects, audit
   - Génération de vrais codes QR avec la librairie qrcode
   - Gestion des erreurs et fallback

   **Modifications clés:**
   - ✅ Import dynamique de qrcode
   - ✅ Interception de fetch avec gestion des paramètres
   - ✅ Réponses JSON valides pour tous les endpoints

### 3. **frontend/package.json** (DÉPENDANCES)
   - ✅ Package `qrcode` installé via npm

---

## Flux d'utilisation final

### Pour les utilisateurs normaux (dev_user, analyst):
1. Login avec credentials
2. Entrer code MFA (n'importe quel 6 chiffres)
3. Dashboard affiche JIT Session TTL et Access Scope
4. Bouton "Submit Request" → Remplir le formulaire → Soumettre
5. Bouton "My Requests" → Voir l'état de toutes leurs requests

### Pour l'admin (admin):
1. Login avec credentials
2. Entrer code MFA
3. Dashboard affiche System Health + Pending Approvals
4. Bouton "Responses" → Voir toutes les requests en attente
5. Cliquer sur une request → Formulaire d'approbation/rejet
6. Ajouter un commentaire et approuver/rejeter
7. L'état se met à jour immédiatement

---

## Données persistantes

Les requests sont sauvegardées dans `localStorage` sous la clé `secureteam_requests`:
```json
[
  {
    "id": 1,
    "username": "user_alpha",
    "requestType": "JIT Request (2h)",
    "timestamp": "14:12:05",
    "description": "...",
    "status": "PENDING|APPROVED|REJECTED",
    "comment": "Admin response here"
  }
]
```

---

## Comptes de test

| Username | Password | Role |
|----------|----------|------|
| admin | password | Security Admin |
| dev_user | password | External Collaborator |
| analyst | password | External Collaborator |

**MFA:** Entrez n'importe quel code à 6 chiffres (ex: 123456)

---

## Endpoints API Mock disponibles

- `GET /api/auth/health` → Status du système
- `GET /api/auth/mfa/setup?username=X` → QR code generation
- `POST /api/auth/mfa/verify` → Vérification OTP
- `GET /api/users` → Liste des users
- `GET /api/projects` → Liste des projets
- `GET /api/audit` → Logs d'audit

---

## Technologies utilisées

- **Frontend:** Lit Web Components (v3)
- **Styling:** CSS-in-JS
- **State Management:** Reactive properties Lit + localStorage
- **QR Codes:** qrcode library (npm)
- **Authentication:** PASETO v2 tokens (mock)
- **API Mock:** Fetch interception

---

## Comment démarrer

```bash
cd frontend
npm install  # Si pas déjà fait
npm run dev  # Démarre le serveur Vite sur http://localhost:5173
```

Puis:
1. Ouvrez http://localhost:5173 dans le navigateur
2. Connectez-vous avec admin/password
3. Entrez 123456 pour MFA
4. Explorez le dashboard et les fonctionnalités
