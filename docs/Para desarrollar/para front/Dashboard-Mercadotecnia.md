# Dashboard Director Mercadotecnia (HI-485)

## Lo que cambia para ti, en una frase

Cuando un usuario con rol `DIRECTOR_MERCADOTECNIA` llame `GET /widgets`, vas a recibir 8 widgets default correspondientes a la HU HI-485, con shapes ya conocidos (`STAT`, `LINE`, `MULTISERIES`, `TABLE`) pero algunos casos especiales que te detallo abajo.

## Endpoints afectados

| Método | Path | Estado | Qué cambia |
|---|---|---|---|
| `GET` | `/widgets` | sin cambio de contrato | empieza a devolver 8 widgets para el rol Mercadotecnia |
| `GET` | `/datasets` | sin cambio de contrato | aparecen 6 datasets nuevos |
| `POST` | `/widgets` | el contrato sigue igual, el JSON `queryConfig` admite dos campos nuevos opcionales | ver sección "Crear widgets personales con doble filtro" |

Todos requieren `Authorization: Bearer <firebase_id_token>` como ya venías mandando. La autenticación, los códigos 401 y el formato de error siguen exactamente como están documentados en `docs/guia para FRONTEND.md`.

## Los 8 widgets default

Si el usuario autenticado tiene rol `DIRECTOR_MERCADOTECNIA`, `GET /widgets` te entrega este array ordenado por `orden` ascendente:

| `orden` | `titulo` | `tipo` | Notas para renderizar |
|---|---|---|---|
| 1 | % Personas con diabetes no diagnosticadas | `STAT` | número en `value`, sin `label` |
| 2 | Estado prioritario por detecciones | `STAT` | `value` es **string** (nombre del estado) |
| 3 | % Población con diabetes (2024) | `STAT` | `value` numérico, `label: "%"` viene en `data` |
| 4 | Carga de diabetes (DALYs) por edad y sexo | `MULTISERIES` | 34 puntos en eje X, 3 series (sex); ver "Filtrado en front" |
| 5 | Detecciones de diabetes por año | `LINE` | 24 puntos (2000–2023) |
| 6 | Obesidad y sobrepeso por edad | `MULTISERIES` | 4 puntos, 6 series con **nombres muy largos**; ver "Etiquetas cortas" |
| 7 | Carencias sociales por municipio | `TABLE` | 15 filas × 7 columnas, pensada para **renderizar como heatmap** |
| 8 | Top 5 estados con más detecciones | `TABLE` | 5 filas × 2 columnas |

### Shape común de la respuesta

Cada elemento del array es un `WidgetResponseDto`:

```ts
type WidgetResponseDto = {
  id: string;                      // UUID
  titulo: string;
  subtitulo?: string;              // "Fuente: <texto>" si el dataset tiene fuente
  tipo: "STAT" | "LINE" | "BAR" | "PIE" | "TABLE" | "MULTISERIES";
  orden: number;
  seriesName?: string;             // nombre legible para el eje Y o leyenda
  xAxisLabel?: string;
  yAxisLabel?: string;             // si hay unidad, viene como "Nombre (unidad)"
  data: Record<string, unknown>;   // shape específico por tipo, ver más abajo
};
```

`subtitulo`, `seriesName`, `xAxisLabel`, `yAxisLabel` se completan automáticamente cuando el dataset/métrica tiene metadatos en BD; si no los hay vienen ausentes o vacíos.

## Detalle por widget

### orden 1 — % Personas con diabetes no diagnosticadas (STAT)

```json
{
  "id": "394e9d09-4d6a-11f1-a630-00155d88026e",
  "orden": 1,
  "tipo": "STAT",
  "titulo": "% Personas con diabetes no diagnosticadas",
  "data": { "value": 38.35 }
}
```

Renderizalo como StatCard mostrando `38.35%`. El backend ya calcula `100 − cobertura de tratamiento` y entrega el complemento directamente.

### orden 2 — Estado prioritario por detecciones (STAT con valor textual)

```json
{
  "orden": 2,
  "tipo": "STAT",
  "titulo": "Estado prioritario por detecciones",
  "data": { "value": "México Oriente" }
}
```

**Caso especial.** `data.value` es **string**, no número. Tu componente StatCard tiene que aceptar `value: number | string`. No vienen unidades.

### orden 3 — % Población con diabetes (2024) (STAT con label)

```json
{
  "orden": 3,
  "tipo": "STAT",
  "titulo": "% Población con diabetes (2024)",
  "subtitulo": "Fuente: International Diabetes Federation",
  "data": { "value": 16.4, "label": "%" }
}
```

El campo `data.label` lleva la unidad (proviene de la `Metrica.unidad` del dataset). Pintalo concatenado al valor (`16.4%`).

### orden 4 — Carga de diabetes (DALYs) por edad y sexo (MULTISERIES)

```json
{
  "orden": 4,
  "tipo": "MULTISERIES",
  "titulo": "Carga de diabetes (DALYs) por edad y sexo",
  "subtitulo": "Fuente: WHO Global Burden of Disease",
  "seriesName": "Valor",
  "xAxisLabel": "Grupo edad",
  "yAxisLabel": "Valor",
  "data": {
    "seriesKeys": ["Both sexes", "Female", "Male"],
    "data": [
      { "label": "20-24 years", "Both sexes": 267.71, "Female": 278.63, "Male": 256.74 },
      { "label": "25-29 years", "Both sexes": 485.73, "Female": 474.03, "Male": 497.84 },
      { "label": "30-34 years", "Both sexes": 769.56, "Female": 726.97, "Male": 814.68 }
      // ... 34 entries
    ]
  }
}
```

#### Filtrado en front (importante)

`data.data` trae **34 grupos de edad** que provienen del dataset PAHO/GBD e incluyen grupos solapados y agregados:

- Granulares (quinquenales): `20-24 years`, `25-29 years`, ..., `75-79 years`, `80-84 years`, `85+ years`.
- Bloques pediátricos: `<1 year`, `0-4 years`, `1-4 years`, `1-11 months`, `0-14 years`, `0-19 years`, `0-24 years`, `10-24 years`, `15-19 years`, `15-24 years`, `1-9 years`, `5-9 years`, `10-14 years`.
- Bloques adultos amplios: `25-49 years`, `50-64 years`, `65-74 years`, `75+ years`.
- Agregados: `All ages`, `Age-standardized`.

Para una gráfica de barras agrupada legible te recomiendo filtrar a quinquenales adultos:

```ts
const adultQuinquennials = [
  "20-24 years","25-29 years","30-34 years","35-39 years","40-44 years",
  "45-49 years","50-54 years","55-59 years","60-64 years","65-69 years",
  "70-74 years","75-79 years","80-84 years","85+ years"
];
const filtered = data.data.filter(d => adultQuinquennials.includes(d.label as string));
```

Si querés mostrar la pediatría aparte, podés hacer un segundo grupo con `0-4 years`, `5-9 years`, `10-14 years`, `15-19 years`. La decisión es tuya.

`seriesKeys` te dice qué claves usar como series en Recharts (`Both sexes`, `Female`, `Male`).

### orden 5 — Detecciones de diabetes por año (LINE)

```json
{
  "orden": 5,
  "tipo": "LINE",
  "titulo": "Detecciones de diabetes por año",
  "subtitulo": "Fuente: SSA / SINAVE",
  "seriesName": "Detecciones",
  "xAxisLabel": "Año",
  "yAxisLabel": "Detecciones (casos)",
  "data": {
    "labels": [2000, 2001, 2002, /* ... */ 2023],
    "values": [5185103, 5230196, 5361306, /* ... */ 0]
  }
}
```

Shape clásico de LINE: arrays paralelos. 24 puntos. Eje Y es count absoluto, no porcentaje.

### orden 6 — Obesidad y sobrepeso por edad (MULTISERIES con nombres largos)

```json
{
  "orden": 6,
  "tipo": "MULTISERIES",
  "titulo": "Obesidad y sobrepeso por edad",
  "subtitulo": "Fuente: PAHO / OPS",
  "yAxisLabel": "Valor (%)",
  "data": {
    "seriesKeys": [
      "Prevalence of obesity among children and adolescents, BMI > +2 standard deviations above the median (crude estimate) (%)",
      "Prevalence of overweight among children and adolescents, BMI > +1 standard deviation above the median (crude estimate) (%)",
      "Prevalence of obesity among adults, BMI ≥ 30 kg/m² (age-standardized estimate) (%)",
      "Prevalence of obesity among adults, BMI ≥ 30 kg/m² (crude estimate) (%)",
      "Prevalence of overweight among adults, BMI ≥ 25 kg/m² (age-standardized estimate) (%)",
      "Prevalence of overweight among adults, BMI ≥ 25 kg/m² (crude estimate) (%)"
    ],
    "data": [
      { "label": "5-9 years",   /* claves largas con valores */ },
      { "label": "10-19 years", /* ... */ },
      { "label": "5-19 years",  /* ... */ },
      { "label": "20+ years",   /* ... */ }
    ]
  }
}
```

#### Etiquetas cortas (recomendado)

Los `seriesKeys` son los nombres oficiales del indicador y son inservibles para una leyenda. Hacé un mapping en el front:

```ts
const seriesLabelMap: Record<string, string> = {
  "Prevalence of obesity among children and adolescents, BMI > +2 standard deviations above the median (crude estimate) (%)": "Obesidad infantil",
  "Prevalence of overweight among children and adolescents, BMI > +1 standard deviation above the median (crude estimate) (%)": "Sobrepeso infantil",
  "Prevalence of obesity among adults, BMI ≥ 30 kg/m² (age-standardized estimate) (%)": "Obesidad adultos (ajustada)",
  "Prevalence of obesity among adults, BMI ≥ 30 kg/m² (crude estimate) (%)": "Obesidad adultos (cruda)",
  "Prevalence of overweight among adults, BMI ≥ 25 kg/m² (age-standardized estimate) (%)": "Sobrepeso adultos (ajustada)",
  "Prevalence of overweight among adults, BMI ≥ 25 kg/m² (crude estimate) (%)": "Sobrepeso adultos (cruda)"
};
```

Los age groups `5-19 years` y `5-9 years`/`10-19 years` se solapan parcialmente. Decide en front cuáles dejar (sugerido: dejar `5-9 years`, `10-19 years`, `20+ years` y omitir `5-19 years`).

### orden 7 — Carencias sociales por municipio (TABLE → heatmap)

```json
{
  "orden": 7,
  "tipo": "TABLE",
  "titulo": "Carencias sociales por municipio",
  "data": {
    "columns": [
      "municipio",
      "rezago_educativo",
      "acceso_salud",
      "acceso_seguridad_social",
      "calidad_vivienda",
      "servicios_basicos",
      "acceso_alimentacion"
    ],
    "rows": [
      { "municipio": "Álvaro Obregón", "rezago_educativo": 2.1, "acceso_salud": 2.3, "acceso_seguridad_social": 1.8, "calidad_vivienda": 2.4, "servicios_basicos": 2.2, "acceso_alimentacion": 2.1 },
      { "municipio": "Chimalhuacán",   "rezago_educativo": 2.7, "acceso_salud": 2.7, "acceso_seguridad_social": 2.1, "calidad_vivienda": 2.8, "servicios_basicos": 3.1, "acceso_alimentacion": 2.6 }
      // ... 15 municipios en total
    ]
  }
}
```

A nivel backend es un `TABLE` clásico. La intención semántica del widget es **heatmap**: eje Y = municipio (15), eje X = las 6 carencias, color = valor del índice multiplicativo (mayor = más carencia). Mapealo a tu componente de heatmap o pintalo como tabla con celdas coloreadas. Los valores están entre ~1.7 y ~3.6.

Cobertura del dataset: 15 municipios de Ciudad de México y Estado de México (no nacional). Pensálo como un widget "ejemplo de mapa de carencias", no como vista geográfica completa.

### orden 8 — Top 5 estados con más detecciones (TABLE)

```json
{
  "orden": 8,
  "tipo": "TABLE",
  "titulo": "Top 5 estados con más detecciones",
  "data": {
    "columns": ["estado", "detecciones_total"],
    "rows": [
      { "estado": "México Oriente",   "detecciones_total": 1502022 },
      { "estado": "Nuevo León",       "detecciones_total": 1339957 },
      { "estado": "D.F. Sur",         "detecciones_total": 1304604 },
      { "estado": "Jalisco",          "detecciones_total": 1109538 },
      { "estado": "Baja California",  "detecciones_total":  783025 }
    ]
  }
}
```

Tabla ranking 5 × 2, ya ordenada desc por `detecciones_total`. Las "delegaciones" del IMSS no son exactamente las 32 entidades federativas (aparecen "México Oriente", "D.F. Sur", etc.); presentalo como "delegación / región IMSS" si querés ser preciso o como "estado" si el cliente prefiere ese rótulo.

## Datasets nuevos disponibles

Si tu UI permite que el usuario navegue datasets (por ejemplo para armar widgets personales), `GET /datasets` ahora incluye estos 6 además de los previos:

| `nombreTabla` | `nombre` | Fuente | Filas |
|---|---|---|---|
| `f5_diabetes_delegaciones` | Detecciones de diabetes por estado y año | SSA / SINAVE | 840 |
| `f12_idf_mexico_porcentajes` | Indicadores IDF México (porcentajes) | International Diabetes Federation | 6 |
| `f2_pobreza_diabetes_2015` | Carencias sociales por municipio 2015 | CONEVAL 2015 | 26 |
| `f7_burden_diabetes` | Carga de diabetes por edad y sexo (GBD) | WHO Global Burden of Disease | 8 976 |
| `f8_prevalence_treatment_diabetes` | Prevalencia y tratamiento de diabetes (NCD-RisC) | NCD-RisC | 594 |
| `f9_obesidad_paho` | Obesidad y sobrepeso por edad y sexo (PAHO) | PAHO / OPS | 990 |

Las métricas (columnas) de cada uno las pedís con `GET /datasets/{id}/metricas` como ya hacés.

## Crear widgets personales con doble filtro (opcional)

Si tu UI permite construir el `queryConfig` de un widget personal (`POST /widgets`), el JSON ahora acepta **un segundo filtro** opcional para `STAT` y `MULTISERIES`. Es 100% compatible hacia atrás: si no mandás los campos nuevos, todo funciona como antes.

### STAT con doble filtro

```json
{
  "titulo": "% Población con diabetes (2024)",
  "tipoId": 1,
  "orden": 0,
  "queryConfig": "{\"tabla\":\"f12_idf_mexico_porcentajes\",\"funcion\":\"MAX\",\"columna\":\"value\",\"filtroCol\":\"indicator\",\"filtroVal\":\"Age-standardised prevalence of diabetes (%)\",\"filtroCol2\":\"year\",\"filtroVal2\":\"2024\"}"
}
```

Campos del JSON serializado en `queryConfig`:

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `tabla` | string | sí | nombreTabla del dataset (o nombre de vista) |
| `funcion` | string | sí | `MAX`, `MIN`, `AVG`, `SUM`, `COUNT` |
| `columna` | string | sí | columna sobre la que se agrega |
| `filtroCol` / `filtroVal` | string | no | filtro 1 (existente) |
| `filtroCol2` / `filtroVal2` | string | no | **nuevo**: filtro 2 combinado con AND |

### MULTISERIES con doble filtro

```json
{
  "titulo": "Carga de diabetes (DALYs) por edad y sexo",
  "tipoId": 6,
  "orden": 0,
  "queryConfig": "{\"tabla\":\"f7_burden_diabetes\",\"colX\":\"age_group\",\"colY\":\"value\",\"colSerie\":\"sex\",\"funcion\":\"MAX\",\"filtroCol\":\"measure_name\",\"filtroVal\":\"Disability-Adjusted Life Years (DALYs)\",\"filtroCol2\":\"year\",\"filtroVal2\":\"2021\"}"
}
```

Mismos campos `filtroCol2/filtroVal2` opcionales. `LINE`/`BAR`/`PIE`/`TABLE` siguen con su shape de siempre, sin doble filtro por ahora.

## Flujo end-to-end

```
1. Usuario hace login Firebase en el front
2. Front obtiene idToken con user.getIdToken()
3. GET /auth/me  → confirma rol DIRECTOR_MERCADOTECNIA
4. GET /widgets  → array de 8 WidgetResponseDto (ordenado por orden 1..8)
5. Por cada widget, según tipo:
   - STAT        → renderizar StatCard con data.value (puede ser string)
   - LINE        → recharts LineChart con data.labels/data.values
   - MULTISERIES → recharts BarChart o LineChart agrupado con data.data y data.seriesKeys
   - TABLE       → tabla / heatmap con data.columns y data.rows
```

## Ejemplo cURL

```bash
TOKEN="<firebase_id_token_de_un_user_DIRECTOR_MERCADOTECNIA>"
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/widgets | jq .
```

## Notas de migración

- Si tu StatCard actual asume `value` numérico, ajustalo para aceptar string (caso del widget orden 2).
- Si listás `seriesKeys` directamente en la leyenda de la gráfica, considerá el mapping de etiquetas cortas para el widget orden 6 (los nombres oficiales pasan los 150 caracteres y rompen layouts).
- Si tu UI muestra todos los `data.data` de un MULTISERIES como vienen, en el widget orden 4 vas a ver age groups solapados; lo ideal es filtrar a quinquenales adultos.

## Pendientes / no cubierto

- No existe todavía el dashboard default del rol `DIRECTOR_GENERAL` (rol_id=2). Si tu UI lo asume, va a recibir array vacío para ese rol.
- El widget de "carencias sociales" solo cubre 15 municipios de CDMX y Estado de México (limitación del dataset CONEVAL 2015 cargado). Si más adelante se sube una versión nacional, el widget la consume automáticamente.
- "Mapa de calor" se entrega como `TABLE`; no hay tipo `HEATMAP` formal en el backend. Si más adelante se decide formalizarlo, se agregará un `tipo_id` nuevo y este doc se actualiza.
