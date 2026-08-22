import React, { HTMLAttributes } from 'react';

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {
  width?: string | number;
  height?: string | number;
  borderRadius?: string | number;
}

export function Skeleton({
  className = '',
  width,
  height,
  borderRadius,
  style,
  ...props
}: SkeletonProps) {
  const mergedStyle = {
    width,
    height,
    borderRadius,
    ...style,
  };

  return (
    <div
      className={`skeleton ${className}`.trim()}
      style={mergedStyle}
      {...props}
    />
  );
}
