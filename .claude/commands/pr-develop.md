---
description: Commit cambios de la rama actual, push a origin y abre PR a develop con el template del repo
---

Tu tarea es hacer shipping de los cambios de la rama actual contra `develop`. Optimiza por velocidad: batch todos los comandos de inspección en paralelo y NO leas diffs archivo por archivo — con commit messages + lista de archivos + `--stat` ya tienes suficiente para llenar el template.

## Paso 0 — Inspección (UN solo bloque paralelo)

Corre EN PARALELO, en un solo mensaje con múltiples tool calls:

```
git status
git log --oneline -5
git log develop..HEAD --oneline
git diff --name-status develop...HEAD
git diff develop...HEAD --stat
git remote -v
```

- Si `git status` muestra cambios sin commitear → Paso 1.
- Si no hay cambios pendientes pero `git log develop..HEAD` tiene commits → salta directo al Paso 2.
- Si `git remote -v` no coincide con la org esperada (ej. repo movido a otra org), recuérdalo para los Pasos 2 y 3 — vas a necesitar el URL/slug correcto.

**NO leas el contenido de diffs individuales de cada archivo.** El body del PR se arma con los commit messages + la lista de archivos + una línea corta por archivo. Solo lee un diff específico si hay ambigüedad real que no puedes resolver con esa info.

## Paso 1 — Commit (solo si hay cambios pendientes)

- Stagea archivos específicos por nombre (no uses `git add -A` / `git add .`).
- Nunca commitees `.claude/settings.local.json` (settings locales del usuario). `.claude/commands/*.md` sí se puede si el usuario lo pide.
- Mensaje en español, sigue el estilo del `git log --oneline -5` que ya corriste. Enfatiza el **por qué**.

## Paso 2 — Push

- `git push` si ya tiene upstream; si no, `git push -u origin HEAD`.
- **Si el output dice `This repository moved. Please use the new location: https://github.com/<NEW_ORG>/<REPO>.git`**: el remote `origin` está desactualizado y `gh pr create` NO sigue redirects. Haz esto de inmediato (sin tocar `git config`):
  1. `git push https://github.com/<NEW_ORG>/<REPO>.git HEAD:<nombre-rama>` para asegurar que la rama existe en el repo nuevo.
  2. En el Paso 3 usa SIEMPRE `--repo <NEW_ORG>/<REPO> --head <nombre-rama>` (solo el nombre de rama, sin prefix `org:`).
  3. Avísale al usuario al final que su remote `origin` sigue apuntando al repo viejo y ofrece actualizarlo con `git remote set-url` si quiere.

## Paso 3 — Pull Request

Forma base:

```
gh pr create --base develop --title "<título>" --body "$(cat <<'EOF'
...body...
EOF
)"
```

Si el repo se movió en el Paso 2, agrega `--repo <NEW_ORG>/<REPO> --head <nombre-rama>`.

- **Título**: < 70 chars, español, estilo `tipo: descripción` (`feat:`, `fix:`, `refactor:`, `chore:`, etc.). Resume el cambio principal de la rama, no de un commit específico.
- **Body**: usa EXACTAMENTE este template:

```markdown
## ¿Qué hace este PR?

<descripción breve de qué implementa o cambia>

## ¿Por qué?

<contexto: historia de usuario, RNF, o problema que resuelve>

## ¿Cómo?

<decisiones técnicas relevantes; usa subsecciones si hay varias partes>

## Archivos

- `ruta/al/archivo` ← nuevo / modificado — qué hace

## Screenshots

_Pendiente de agregar. Para backend: respuestas en Postman. Para frontend: flujo visual._

## Relacionado

HU__
```

### Reglas para llenar el template

- **Archivos**: UN item por archivo de `git diff --name-status develop...HEAD`. Marca `nuevo` (status `A`) o `modificado` (status `M`); para `R` usa `renombrado`, para `D` usa `eliminado`. Descripción de una línea por archivo.
- **¿Qué hace este PR?** y **¿Cómo?**: derívalos de los commit messages (`git log develop..HEAD --oneline`) + nombres de archivos. No abras los diffs solo para esto.
- **¿Por qué?**: contexto de negocio/HU. Si el usuario dio contexto en la conversación o en `$ARGUMENTS`, úsalo. Si no, escribe `_Pendiente: completar con el contexto de la historia de usuario._` y avísale al final.
- **Relacionado**: si `$ARGUMENTS` trae un ID (ej. `HI-482`) o se infiere del nombre de la rama (`feature/HI-482-...` → `HU HI-482`), úsalo. Si no, deja `HU__` literal.
- **Screenshots**: deja la nota de pendiente tal cual.
- **NO agregues** "Test plan", "🤖 Generated with Claude Code", ni Co-Authored-By al body del PR.

## Al terminar

- Imprime la URL del PR devuelta por `gh pr create`.
- Lista explícitamente qué secciones quedaron como "Pendiente" y qué necesita el usuario para completarlas.
- Si hubo redirect de repo en el Paso 2, menciónalo y ofrece actualizar `origin`.

## Argumentos del usuario

Si el usuario pasó argumentos al comando, úsalos como contexto adicional (ej. número de HU, resumen del cambio, enlaces):

$ARGUMENTS
