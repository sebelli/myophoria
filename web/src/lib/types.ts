export type EquipmentType = 'BARBELL' | 'DUMBBELL' | 'STACK' | 'PLATE_LOADED' | 'CALISTHENICS'
export type Laterality = 'BILATERAL' | 'UNILATERAL'

export const EQUIPMENT_TYPES: EquipmentType[] = ['BARBELL', 'DUMBBELL', 'STACK', 'PLATE_LOADED', 'CALISTHENICS']

export interface Exercise {
  id: string
  name: string
  equipmentType: EquipmentType
  laterality: Laterality
  initials: string
  weightLabel: string
  deleted: boolean
}

export interface ExerciseInput {
  name: string
  equipmentType: EquipmentType
  laterality: Laterality
}

export interface ErrorBody {
  error: string
  message: string
  details?: Record<string, unknown>
}
