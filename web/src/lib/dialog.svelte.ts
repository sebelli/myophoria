// One app-wide dialog; screens await the answer instead of managing their own modal state.
export interface Choice<T> {
  label: string
  value: T
  danger?: boolean
}

export const dialog = $state<{
  message: string
  choices: Choice<unknown>[]
  resolve: ((v: unknown) => void) | null
}>({ message: '', choices: [], resolve: null })

/** Shows a message with buttons; resolves to the chosen value, or null when dismissed. */
export function choose<T>(message: string, choices: Choice<T>[]): Promise<T | null> {
  return new Promise((resolve) => {
    dialog.message = message
    dialog.choices = choices as Choice<unknown>[]
    dialog.resolve = resolve as (v: unknown) => void
  })
}

/** Yes/no confirmation. */
export async function confirmAction(message: string, label = 'OK', danger = false): Promise<boolean> {
  return (await choose(message, [{ label, value: true, danger }])) === true
}

export function closeDialog(value: unknown) {
  const r = dialog.resolve
  dialog.resolve = null
  r?.(value)
}
