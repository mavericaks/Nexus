import React, { HTMLAttributes } from 'react';

export interface EmptyStateProps extends HTMLAttributes<HTMLDivElement> {
  icon?: React.ReactNode;
  title: string;
  description?: string;
}

export function EmptyState({
  icon,
  title,
  description,
  children,
  className = '',
  ...props
}: EmptyStateProps) {
  return (
    <div className={`empty-state ${className}`.trim()} {...props}>
      {icon && <div className="empty-state-icon">{icon}</div>}
      <p className="empty-state-title">{title}</p>
      {description && <p className="empty-state-description">{description}</p>}
      {children}
    </div>
  );
}
