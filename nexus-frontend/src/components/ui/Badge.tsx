import React, { HTMLAttributes } from 'react';
import { STATUS_CONFIG, PRIORITY_CONFIG } from '@/lib/constants';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  status?: string;
  priority?: string;
  role?: string;
  customClass?: string;
}

export function Badge({
  children,
  status,
  priority,
  role,
  customClass,
  className = '',
  ...props
}: BadgeProps) {
  const baseClass = 'badge';
  
  let typeClass = '';
  let label = children;

  if (status && STATUS_CONFIG[status]) {
    typeClass = STATUS_CONFIG[status].badgeClass;
    if (!children) label = STATUS_CONFIG[status].label;
  } else if (priority && PRIORITY_CONFIG[priority]) {
    typeClass = PRIORITY_CONFIG[priority].badgeClass;
    if (!children) label = PRIORITY_CONFIG[priority].label;
  } else if (role) {
    typeClass = 'badge--role';
    if (!children) label = role;
  } else if (customClass) {
    typeClass = customClass;
  }

  const classes = [baseClass, typeClass, className].filter(Boolean).join(' ');

  return (
    <span className={classes} {...props}>
      {label}
    </span>
  );
}
