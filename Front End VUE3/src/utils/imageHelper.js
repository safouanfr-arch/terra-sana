import { getApiBaseUrl } from '../services/api.js'

function escapeXml(value = '') {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

export function productPlaceholder(name = 'Produit', categorie = '') {
  const label = escapeXml((name || 'Produit').toString().trim().slice(0, 30) || 'Produit')
  const categoryLabel = escapeXml((categorie || 'Selection locale').toString().trim().slice(0, 24) || 'Selection locale')

  const palette = categorie && categorie.toLowerCase().includes('lait')
    ? { base: '#e7eef4', accent: '#6b879b', accentSoft: '#cddbe6' }
    : { base: '#ece7de', accent: '#547364', accentSoft: '#d9dfd7' }

  const svg = `<?xml version="1.0" encoding="UTF-8"?>
  <svg xmlns="http://www.w3.org/2000/svg" width="800" height="560" viewBox="0 0 800 560">
    <defs>
      <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="${palette.base}"/>
        <stop offset="100%" stop-color="#f8f4ec"/>
      </linearGradient>
    </defs>
    <rect x="0" y="0" width="800" height="560" fill="url(#bg)"/>
    <circle cx="662" cy="94" r="112" fill="${palette.accentSoft}" opacity="0.75"/>
    <circle cx="126" cy="438" r="146" fill="#f6f1e8" opacity="0.92"/>
    <rect x="74" y="68" width="164" height="40" rx="20" fill="rgba(29,37,43,0.06)"/>
    <text x="156" y="93" text-anchor="middle" font-family="'Segoe UI', Arial, sans-serif" font-size="20" font-weight="700" fill="#50606d">
      Terra Sana
    </text>
    <rect x="92" y="138" width="616" height="206" rx="34" fill="rgba(255,255,255,0.48)" stroke="rgba(29,37,43,0.08)" stroke-width="2"/>
    <circle cx="238" cy="242" r="64" fill="${palette.accent}" opacity="0.16"/>
    <circle cx="404" cy="228" r="54" fill="${palette.accent}" opacity="0.11"/>
    <circle cx="548" cy="256" r="72" fill="${palette.accent}" opacity="0.14"/>
    <path d="M214 308c34-58 84-106 156-110 76-4 128 36 186 118" fill="none" stroke="${palette.accent}" stroke-width="14" stroke-linecap="round" opacity="0.26"/>
    <path d="M284 298c24-34 58-60 100-60 46 0 82 22 120 62" fill="none" stroke="${palette.accent}" stroke-width="10" stroke-linecap="round" opacity="0.34"/>
    <text x="92" y="414" font-family="'Segoe UI', Arial, sans-serif" font-size="20" font-weight="700" letter-spacing="2" fill="#6b746c">
      ${categoryLabel.toUpperCase()}
    </text>
    <text x="92" y="470" font-family="'Segoe UI', Arial, sans-serif" font-size="44" font-weight="700" fill="#1d252b">
      ${label}
    </text>
    <text x="92" y="508" font-family="'Segoe UI', Arial, sans-serif" font-size="22" fill="#66717a">
      Produit disponible dans le catalogue Terra Sana
    </text>
  </svg>`

  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

export function getProductImageSrc(produit) {
  const candidate = produit?.image
  const resolved = resolveProductImageSrc(candidate)
  if (resolved) {
    return resolved
  }
  return productPlaceholder(produit?.nom, produit?.categorie)
}

export function resolveProductImageSrc(candidate) {
  if (typeof candidate !== 'string' || !candidate.trim()) {
    return ''
  }

  const value = candidate.trim()
  if (value.startsWith('http') || value.startsWith('data:image/')) {
    return value
  }

  if (value.startsWith('/')) {
    return `${getApiBaseUrl().replace(/\/api$/, '')}${value}`
  }

  return ''
}
