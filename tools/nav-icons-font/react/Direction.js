import React from 'react';
export const Direction = props => (
  <svg viewBox="0 0 20 20"  {...props} className={`sacred_direction_nav ${props.className ? props.className : ''}`}><path d="M9,0C4.028,0,0,4.028,0,9s4.028,9,9,9s9-4.028,9-9S13.972,0,9,0z M9,14.538V9H3.475l9.679-4.154L9,14.538z" fillRule="evenodd" /></svg>
);
