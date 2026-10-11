import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/axios';

/** GET a list; returns { data, error, loading, reload }. Params are compared by value. */
export function useApiList(path, params) {
  const [state, setState] = useState({ data: null, error: '' });
  const [tick, setTick] = useState(0);
  const key = JSON.stringify(params || {});

  useEffect(() => {
    let active = true;
    api.get(path, { params: JSON.parse(key) })
      .then((res) => active && setState({ data: res.data.data, error: '' }))
      .catch((err) => active && setState((s) => ({ ...s, error: errorMessage(err) })));
    return () => { active = false; };
  }, [path, key, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { data: state.data, error: state.error, loading: !state.data && !state.error, reload };
}