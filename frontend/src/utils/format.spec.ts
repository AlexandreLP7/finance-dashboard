import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate } from './format'

describe('formatCurrency', () => {
  it('formate un montant en EUR avec 2 decimales', () => {
    expect(formatCurrency(1234.5)).toBe('1 234,50 €')
  })

  it('formate un montant negatif', () => {
    expect(formatCurrency(-42)).toContain('-')
    expect(formatCurrency(-42)).toContain('42,00')
  })

  it('accepte une autre devise', () => {
    expect(formatCurrency(10, 'USD')).toContain('$')
  })
})

describe('formatDate', () => {
  it('formate une date ISO en fr-FR', () => {
    expect(formatDate('2026-01-15')).toBe('15/01/2026')
  })

  it('retourne la chaine brute si la date est invalide', () => {
    expect(formatDate('pas-une-date')).toBe('pas-une-date')
  })
})
