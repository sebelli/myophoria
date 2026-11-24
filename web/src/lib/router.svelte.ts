// Hash routing without a router library (G9): '#/history/abc' → ['history', 'abc'].
function parse(): string[] {
  return location.hash.replace(/^#\/?/, '').split('/').filter(Boolean).map(decodeURIComponent)
}

export const route = $state({ parts: parse() })

window.addEventListener('hashchange', () => {
  route.parts = parse()
  window.scrollTo(0, 0)
})

export function go(path: string) {
  location.hash = '#/' + path
}
