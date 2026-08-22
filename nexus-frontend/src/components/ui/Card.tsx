import React, { HTMLAttributes } from 'react';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  static?: boolean;
  inset?: boolean;
}

export function Card({
  children,
  static: isStatic = false,
  inset = false,
  className = '',
  ...props
}: CardProps) {
  const baseClass = 'glass';
  const staticClass = isStatic ? 'glass--static' : '';
  const insetClass = inset ? 'glass--inset' : '';
  const classes = [baseClass, staticClass, insetClass, className].filter(Boolean).join(' ');

  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
}
