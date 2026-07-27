/** Matches every backend controller's response envelope exactly: { success, message, data }. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/** Matches every backend PagedResponse<T> (Spring Data Page projection). */
export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
