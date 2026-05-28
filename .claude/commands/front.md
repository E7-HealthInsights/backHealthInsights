---
description: Genera un doc de handoff para el frontend en docs/Para desarrollar/para front/ con lo que se construyó en la sesión (endpoints, payloads, flujos, ejemplos)
---

Generá un documento de handoff para el dev de frontend con todo lo necesario para consumir o trabajar contra lo que se construyó/cambió en la sesión actual. Se usa al final de la sesión, una vez que la implementación del backend ya quedó cerrada.

Carpeta destino: `docs/Para desarrollar/para front/`. Si no existe, creala.

## Reglas de estilo

- **Español**, técnico pero claro, sin emojis.
- Tono de handoff: hablale al dev de frontend en segunda persona ("vos vas a llamar...", "lo que cambia para ti es...").
- Encabezados `##` / `###`, tablas markdown para resumir endpoints/payloads, bloques de código con JSON / TypeScript / curl reales.
- Citar rutas reales del backend (`app/api/v1/endpoints/...`) cuando aporte trazabilidad, pero sin pedirle al frontend que las lea — son referencia.
- Cero emojis, cero atribuciones a Claude, cero referencias a "esta sesión" o fechas relativas.

## Pasos

1. **Detectá los cambios del branch.** La fuente de verdad es **tu propia conversación**: repasá el transcript actual y armá la lista de endpoints/contratos/flujos que se crearon o modificaron en esta sesión vía tool calls (`Edit`, `Write`, etc.). Esa es la lista a documentar.

   Si dudás de lo que hiciste o no en esta sesión, **preguntale al usuario** antes de asumir.

   Como ayuda secundaria (no para definir el alcance):
   - Releé los archivos modificados para extraer el shape real de request/response.
   - `git status` / `git diff` solo si hace falta confirmar el estado de un archivo puntual.

2. **Filtrá lo que le importa al frontend.** No todo cambio de backend genera handoff. Cosas que SÍ van al doc:
   - Endpoints nuevos o modificados (path, método, auth requerida, query params, body, response).
   - Cambios en el shape de respuestas que el frontend ya consume.
   - WebSockets / SSE / eventos en tiempo real (canal, formato de mensajes).
   - Nuevos flujos multi-paso (ej. polling de estado de procesamiento, subida + confirmación).
   - Cambios en autenticación o headers requeridos.
   - Errores nuevos relevantes (códigos HTTP + cuerpo de error).

   Lo que NO va: refactors internos, cambios de servicios, optimizaciones, migraciones DB que no afectan el contrato.

   Si el cambio de la sesión no afecta el contrato del frontend, decíselo al usuario y no generes el doc.

3. **Decidí: doc nuevo o actualización.**
   - ¿El feature ya tiene un doc en `docs/Para desarrollar/para front/` o un handoff equivalente en `docs/Para desarrollar/` (ej. `Frontend-Firebase-Auth-Integration.md`)? → **actualizá** el existente.
   - ¿Es un feature/flujo nuevo sin doc previo? → **creá** un archivo nuevo en `docs/Para desarrollar/para front/` con nombre descriptivo en kebab-case o Title-Case consistente con la carpeta (ej. `notas-autosave.md`, `Notas-Autosave.md`).

   Si dudás, preguntá al usuario antes de crear.

4. **Estructura sugerida del doc** (adaptá según el feature):
   - **Lo que cambia para ti, en una frase.** Resumen de una línea.
   - **Endpoints afectados.** Tabla con método, path, estado (nuevo / modificado / deprecado), descripción corta.
   - **Auth y headers.** Qué header mandar, formato del token, scopes si aplica.
   - **Request / Response por endpoint.** Para cada uno: path completo, ejemplo de body, ejemplo de response 2xx, errores típicos (4xx/5xx) con su body.
   - **Flujo end-to-end.** Si el feature requiere varios llamados en orden (ej. crear → subir audio → poll de estado), diagrama ASCII simple o lista numerada.
   - **Ejemplos concretos.** `curl` o snippet TypeScript que el frontend pueda copiar y adaptar.
   - **Notas de migración.** Si el frontend ya estaba consumiendo algo y debe migrar, indicar qué cambia y cómo migrar.
   - **Pendientes / no cubierto.** Qué quedó fuera del alcance y, si se sabe, cuándo se aborda.

5. **Sé fiel al código.** No inventes campos, códigos de error, ni headers. Si un endpoint devuelve `{ "id": "...", "status": "pending" }`, eso es lo que documentás — abrí el archivo del endpoint y mirá el modelo Pydantic / response real.

6. **Resumen final al usuario** (1-2 frases): qué archivo creaste/actualizaste y qué endpoints quedaron cubiertos. No hagas commit ni push — eso es decisión del usuario (`/push` o `/pr` después si quiere).

## Qué NO hacer

- No documentes cosas que no cambiaron en la sesión.
- No documentes cambios internos del backend que no afectan el contrato del frontend.
- No inventes shapes de request/response: copialos del código real.
- No crees un doc nuevo si encaja en uno existente.
- No agregues atribución de Claude ni firmas.
- No commitees automáticamente.
