export function resolveErrorMessage(error, fallbackMessage) {
  const status = error.response?.status;

  if (!error.response) {
    return 'No se pudo contactar al servidor. Revisa tu conexion o intenta mas tarde.';
  }

  if (status === 400) {
    return error.response.data?.message || 'Los datos ingresados no son validos.';
  }
  if (status === 401) {
    return 'Tu sesion no es valida o expiro. Vuelve a iniciar sesion.';
  }
  if (status === 403) {
    return 'No tienes permisos para realizar esta accion.';
  }
  if (status === 503 || status === 502 || status === 504) {
    return 'El servicio no esta disponible en este momento. Intenta nuevamente mas tarde.';
  }
  if (status === 500) {
    return 'Ocurrio un error en el servidor. Intenta nuevamente mas tarde.';
  }
  return fallbackMessage;
}
